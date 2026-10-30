# Course discussions

This feature implements the confirmed FR-28 through FR-35 course discussion scope. Keep pages, API calls, lifecycle state and attachment presentation within this feature; permission and visibility decisions remain in the backend service and Mapper XML.

Read [the maintained handbook](../../../../docs/UP-SELF-Developer-Handbook.md#422-course-discussions-visibility-and-filtering-fr-28-through-fr-35) for group access, teaching-message protection, legacy records, filtering limits, uploads, cursor behavior and future extension boundaries.

`useDiscussion` owns one combined polling request, bounded message windows, stale-response handling and visibility refresh. `ChatRoom` owns the composer and conversation actions. `ChatMembers` owns membership and personal hide/show controls. `ChatAttachment` and the shared recorder/player handle private files and object URL cleanup. Neither UI visibility nor a supplied sender role grants server access.

Run `scripts/verify-chat-safety.py --browser` and `scripts/verify-chat.py --browser` from the repository root with local services running. They create disposable fixtures, check real Chrome playback with a simulated microphone, and remove their own records and uploads. Re-login after V16 to refresh the student hide permission. FR-59 is deferred.
