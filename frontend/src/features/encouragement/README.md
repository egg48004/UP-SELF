# Mentor encouragement

FR-59 provides one-way text delivery from a current course mentor to selected students. Entries live on the existing role home pages and student overview; main navigation is unchanged.

`RecipientPicker` owns recipient search, pagination and selection controls. `useEncouragementComposer` owns draft state, validation, in-flight protection and request keys. `SentMessages` owns author history and recipient detail. `StudentEncouragementPage` displays only the current student's inbox. API adapters, types and English messages stay in this feature; role and relationship enforcement remain on the server.

The home card makes no inbox request. Lists load on entry and explicit refresh. Student payloads omit all other recipients. Reuse the existing Aa preferences and shared async/pagination controls. Preserve an uncertain send draft and its request key; change the key when the payload changes. Only a confirmed send clears it. The shared literal text filter applies, while course-group peer hiding does not affect encouragement.

See [the maintained handbook](../../../../docs/UP-SELF-Developer-Handbook.md#423-mentor-encouragement-fr-59) for delivery and history scopes. Run `scripts/verify-encouragement.py --browser` with local services for disposable API and Chrome checks. V17 adds the two tables and function permissions; existing sessions should sign out and in to refresh permissions.
