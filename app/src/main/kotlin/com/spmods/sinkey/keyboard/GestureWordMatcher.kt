package com.spmods.sinkey.keyboard

/**
 * One on-screen key's center position, used both to build the sampled
 * "ideal path" for a candidate word (see GestureWordMatcher.idealPath) and
 * to find the nearest key under the finger at each point along the user's
 * actual swipe (see GestureTypingOverlay).
 */
internal data class KeyPoint(val char: Char, val x: Float, val y: Float)

/**
 * Matches a swiped touch path against a word list to find the most likely
 * intended word — the core of gesture (swipe-to-type) typing.
 *
 * How it works (a simplified version of the technique behind most gesture
 * keyboards): for every candidate word, build its "ideal path" by
 * connecting the on-screen centers of its letters in order (skipping
 * consecutive repeated letters, since swiping doesn't loop back for a
 * double letter — the finger just pauses over the same key). Then compare
 * that ideal path against the user's actual sampled path using two
 * complementary checks:
 *
 * 1. **Shape score** — resample both paths to the same fixed number of
 *    points and sum the distance between corresponding points. This
 *    rewards words whose letters trace a similar overall route across the
 *    keyboard, and is fairly tolerant of swiping speed/sampling-rate
 *    differences since both paths are normalized to the same point count.
 *
 * 2. **Endpoint + key-coverage score** — the user's swipe start/end should
 *    land near the candidate's first/last letter, and every letter in the
 *    candidate should have at least one nearby point somewhere along the
 *    actual path (a real swipe for "hello" passes near h, e, l, o in that
 *    rough order; a swipe for "world" wouldn't pass near an "h" at all).
 *    This catches cases where two words have superficially similar shapes
 *    but don't actually share letters in the right places.
 *
 * The two scores are combined and the lowest-cost (best-matching)
 * candidates win. This is intentionally simpler than production gesture
 * engines (no language model / bigram weighting of the path itself — that
 * happens afterward, the same way typed-word suggestions already blend in
 * frequency via WordRepository), but is enough to turn a rough swipe into
 * a short, reasonably-ranked candidate list.
 */
internal object GestureWordMatcher {

    private const val RESAMPLE_POINTS = 32

    /**
     * Ranks [candidateWords] by how well they match [swipePath] (raw
     * on-screen touch points, in the same coordinate space as
     * [keyPositions]) and returns the top [limit], best match first.
     * [keyPositions] must cover every letter that could plausibly appear in
     * [candidateWords] — GestureTypingOverlay builds this once per
     * language/layout and reuses it for every swipe.
     *
     * Returns an empty list if the path is too short to be a real gesture
     * (a tap that accidentally triggered gesture mode) or if none of
     * [candidateWords] have letters this key layout even has positions
     * for.
     */
    fun match(
        swipePath: List<KeyPoint>,
        candidateWords: List<String>,
        keyPositions: Map<Char, KeyPoint>,
        limit: Int = 5
    ): List<String> {
        if (swipePath.size < 2 || candidateWords.isEmpty()) return emptyList()

        val resampledSwipe = resample(swipePath.map { it.x to it.y }, RESAMPLE_POINTS)
        val swipeStart = swipePath.first()
        val swipeEnd = swipePath.last()

        val scored = candidateWords.mapNotNull { word ->
            val lower = word.lowercase()
            val idealRaw = idealPath(lower, keyPositions) ?: return@mapNotNull null
            if (idealRaw.size < 2) return@mapNotNull null

            val idealResampled = resample(idealRaw.map { it.x to it.y }, RESAMPLE_POINTS)
            val shapeCost = pathDistance(resampledSwipe, idealResampled)

            val firstKey = keyPositions[lower.first()]
            val lastKey = keyPositions[lower.last()]
            val endpointCost = if (firstKey != null && lastKey != null) {
                distance(swipeStart.x, swipeStart.y, firstKey.x, firstKey.y) +
                    distance(swipeEnd.x, swipeEnd.y, lastKey.x, lastKey.y)
            } else {
                // Word contains a letter this layout has no key for (shouldn't
                // normally happen — candidates are pre-filtered by language —
                // but fail safe rather than crash on a bad candidate).
                Float.MAX_VALUE / 2
            }

            // Every distinct letter in the word should have at least one
            // swipe point that passed reasonably close to its key — this is
            // what tells "hello" and "help" apart even though their paths
            // start similarly, by checking whether the path actually swings
            // near the letters that differ between them.
            val coveragePenalty = lower.toSet().sumOf { ch ->
                val key = keyPositions[ch] ?: return@sumOf MISSING_KEY_PENALTY.toDouble()
                val closest = swipePath.minOf { p -> distance(p.x, p.y, key.x, key.y) }
                (closest * COVERAGE_WEIGHT).toDouble()
            }.toFloat()

            val totalCost = shapeCost + endpointCost * ENDPOINT_WEIGHT + coveragePenalty
            word to totalCost
        }

        return scored.sortedBy { it.second }.take(limit).map { it.first }
    }

    /**
     * The path a perfectly-drawn swipe for [word] would trace: the key
     * center for each letter in order, with consecutive duplicate letters
     * collapsed to one point (swiping "hello"'s double-L doesn't produce
     * two separate path points — the finger just doesn't move for an
     * instant). Returns null if any letter in [word] has no entry in
     * [keyPositions] (e.g. a candidate word contains a character outside
     * this layout, such as punctuation slipping into the word list).
     */
    private fun idealPath(word: String, keyPositions: Map<Char, KeyPoint>): List<KeyPoint>? {
        val points = mutableListOf<KeyPoint>()
        for (ch in word) {
            val key = keyPositions[ch] ?: return null
            if (points.isEmpty() || points.last().char != ch) points.add(key)
        }
        return points
    }

    /**
     * Resamples [points] to exactly [count] evenly-arc-length-spaced
     * points along the polyline connecting them, so two paths with
     * different numbers of original points (a fast swipe samples fewer raw
     * touch events than a slow one; a short word's ideal path has fewer
     * letters than a long one) become directly comparable point-for-point.
     */
    private fun resample(points: List<Pair<Float, Float>>, count: Int): List<Pair<Float, Float>> {
        if (points.size == 1) return List(count) { points[0] }
        val totalLength = points.zipWithNext { a, b -> distance(a.first, a.second, b.first, b.second) }.sum()
        if (totalLength == 0f) return List(count) { points[0] }

        val interval = totalLength / (count - 1)
        val result = mutableListOf(points.first())
        var accumulated = 0f
        var segmentStart = points.first()

        var i = 1
        while (result.size < count && i < points.size) {
            val segmentEnd = points[i]
            val segmentLength = distance(segmentStart.first, segmentStart.second, segmentEnd.first, segmentEnd.second)
            if (segmentLength == 0f) { i++; segmentStart = segmentEnd; continue }

            if (accumulated + segmentLength >= interval) {
                val remaining = interval - accumulated
                val t = remaining / segmentLength
                val nx = segmentStart.first + (segmentEnd.first - segmentStart.first) * t
                val ny = segmentStart.second + (segmentEnd.second - segmentStart.second) * t
                result.add(nx to ny)
                segmentStart = nx to ny
                accumulated = 0f
            } else {
                accumulated += segmentLength
                segmentStart = segmentEnd
                i++
            }
        }
        while (result.size < count) result.add(points.last())
        return result
    }

    private fun pathDistance(a: List<Pair<Float, Float>>, b: List<Pair<Float, Float>>): Float {
        var sum = 0f
        for (i in a.indices) sum += distance(a[i].first, a[i].second, b[i].first, b[i].second)
        return sum
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private const val ENDPOINT_WEIGHT = 1.5f
    private const val COVERAGE_WEIGHT = 0.15f
    private const val MISSING_KEY_PENALTY = 200
}
