# UP SELF frontend

Vue 3 and TypeScript frontend for the existing Java backend. Students use a dedicated learning layout; mentors and administrators use role-specific management screens and shared components. Public roles are Student, Mentor and Administrator. Maintenance is an internal entry only.

For fixed versions, directory boundaries, permissions, current workflows and setup, use the [Developer Handbook](../docs/UP-SELF-Developer-Handbook.md). The API reference is in Appendix B. Student accessibility rules are in Section 2.11.

On Windows, run the existing frontend.cmd commands from the repository root. On Mac, use scripts/macos/up-self.sh after configuring the pinned tools. Install dependencies with the lockfile rather than copying node_modules between computers.

The development interface is http://127.0.0.1:5173/ and forwards /api to the Java backend on 8080. Production uses built dist files, not the Vite development server. Mac and Linux runtime acceptance remain separate from recorded Windows browser tests.
