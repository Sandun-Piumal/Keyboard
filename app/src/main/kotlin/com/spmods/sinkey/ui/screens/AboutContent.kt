package com.spmods.sinkey.ui.screens

/**
 * Static text content for the About section's legal pages and user guide.
 * Kept as plain Kotlin string constants (not resource XML) since
 * LegalTextScreen/UserGuideScreen just need one long string each and this
 * avoids the extra indirection of strings.xml for content this long and
 * this rarely referenced from anywhere else.
 *
 * IMPORTANT: PRIVACY_POLICY and TERMS_AND_CONDITIONS below are a starting
 * draft, not a substitute for actual legal review. They describe SinKey's
 * behavior as currently implemented elsewhere in this codebase (personal
 * dictionary/quick text stored locally via Room, no network calls found
 * anywhere in the app's source, no analytics/ad SDKs in build.gradle.kts) —
 * but a keyboard app handles everything the user types, which many
 * jurisdictions (and app store policies) subject to specific disclosure
 * requirements. Before publishing, these should be reviewed by someone
 * qualified to confirm they're accurate and sufficient for wherever the
 * app is distributed.
 */
object AboutContent {

    const val PRIVACY_POLICY = """
Privacy Policy

Last updated: 2026

This Privacy Policy explains what information SinKey collects, how it is used, and the choices you have.

1. Data collected

SinKey is a keyboard app. To function, it needs to process the text you type. This includes:
• Words you type, used to build your personal dictionary and suggest words you've typed before.
• Quick text shortcuts you create.
• Keyboard preferences such as theme, sound, and layout settings.

2. Where your data is stored

All of the data above is stored only on your device, in the app's local storage. SinKey does not send what you type to any external server.

3. Sensitive fields

Like most keyboards, SinKey avoids learning words typed into password fields and other fields marked sensitive by the app you're typing into, where the operating system makes that information available.

4. Incognito mode

When Incognito mode is turned on in Settings, SinKey does not learn or store any new words while it's active.

5. Permissions

SinKey may request permissions such as microphone access (for voice input, if used) or storage access (for custom keyboard backgrounds). These are used only for the feature you enable them for.

6. Third parties

SinKey does not share your typed data with third parties. The app does not include advertising or analytics SDKs.

7. Your choices

You can clear your personal dictionary, delete individual words, or delete quick text shortcuts at any time from Settings. Uninstalling the app removes all locally stored data.

8. Changes to this policy

This policy may be updated from time to time. Continued use of the app after a change means you accept the revised policy.

9. Contact

Questions about this policy can be sent to the developer — see About → About developer for contact links.
"""

    const val TERMS_AND_CONDITIONS = """
Terms & Conditions

Last updated: 2026

By installing and using SinKey, you agree to the following terms.

1. License

SinKey is provided for your personal use on devices you own or control. You may not redistribute, resell, or repackage the app without permission.

2. Acceptable use

You agree not to use SinKey for any unlawful purpose, and not to attempt to reverse engineer, decompile, or tamper with the app beyond what is permitted by applicable law.

3. No warranty

SinKey is provided "as is", without warranty of any kind. The developer does not guarantee the app will be error-free, uninterrupted, or fit for any particular purpose.

4. Limitation of liability

To the fullest extent permitted by law, the developer is not liable for any indirect, incidental, or consequential damages arising from your use of the app.

5. Changes to the app

Features may be added, changed, or removed in future updates. Settings and stored data (personal dictionary, quick text shortcuts) are preserved across updates where technically possible, but this is not guaranteed for major changes.

6. Termination

You may stop using SinKey at any time by disabling or uninstalling it. These terms remain in effect for as long as you use the app.

7. Governing law

These terms are governed by applicable local law where the app is distributed, without regard to conflict-of-law principles.

8. Contact

Questions about these terms can be sent to the developer — see About → About developer for contact links.
"""

    const val USER_GUIDE = """
Getting started

SinKey lets you type Sinhala using English letters (transliteration), plain English, or a mix of both — switch any time from the language key on the keyboard.

Typing Sinhala

Type the sounds of the word in English letters and SinKey converts it as you type. For example, typing "kohomada" produces "කොහොමද". If a word doesn't convert the way you expect, tap the suggestion bar to see alternate spellings.

Switching modes

Tap the language key to cycle between Sinhala, English, and Mix mode. Mix mode lets you type Sinhala and English in the same sentence without switching back and forth manually.

Quick text shortcuts

Under Settings → Quick text, you can set up short triggers that expand into longer phrases as you type — for example "gm" expanding to "Good morning". Turn the feature on, tap + to add a shortcut, type the trigger text and what it should expand to, then save. Typing the trigger followed by a space or enter will expand it automatically.

Personal dictionary

SinKey learns words you type so it can suggest them again later. Under Settings → Personal dictionary you can browse everything it has learned (split into Sinhala and English tabs), remove a word you don't want suggested anymore, or add a word by hand so it's suggested right away instead of waiting for you to type it a few times first.

Swipe typing

If enabled in Settings, you can drag your finger across the keys instead of tapping each letter individually — works for both Sinhala and English.

Customizing the keyboard

Under the Themes tab you can change the keyboard's colors, background (including your own photo), key sound and vibration, height, and more.

Enabling the keyboard

SinKey needs to be enabled in your device's keyboard settings and selected as your default keyboard before you can use it in other apps. This is a two-step process — enabling the keyboard, then selecting it as default — and can be done from the Home tab's setup card at any time, or by replaying the onboarding tutorial from About → Show tutorial again.

Need more help?

If something isn't covered here, see About → About developer for ways to reach out.
"""
}
