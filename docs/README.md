# UP SELF documentation

The [Developer Handbook](UP-SELF-Developer-Handbook.md) is the maintained documentation source. The [Word edition](UP-SELF-Developer-Handbook.docx) contains a populated contents page, internal navigation and page numbers. It is the published 25 October edition; the Markdown source includes the later V16 changes.

| Read about | Handbook section |
|---|---|
| Scope, directories and fixed versions | [Project overview](UP-SELF-Developer-Handbook.md#1-project-overview) |
| Java and Vue development | [Architecture and standards](UP-SELF-Developer-Handbook.md#2-architecture-and-development-standards) |
| Roles, login and privacy | [Authentication and privacy](UP-SELF-Developer-Handbook.md#3-roles-authentication-and-privacy) |
| Current feature behavior | [Feature workflows](UP-SELF-Developer-Handbook.md#4-feature-workflows) |
| Windows, Mac, database and files | [Setup and migration](UP-SELF-Developer-Handbook.md#5-local-setup-and-data-migration) |
| Linux, purchase order, DNS and budget | [Deployment and costs](UP-SELF-Developer-Handbook.md#6-deployment-and-operating-costs) |
| Tests and graduation demonstration | [Verification and demonstration](UP-SELF-Developer-Handbook.md#7-verification-and-demonstration) |
| All 61 report requirements | [FR matrix](UP-SELF-Developer-Handbook.md#appendix-a-functional-requirements) |
| Current controller routes and requests | [API reference](UP-SELF-Developer-Handbook.md#appendix-b-api-reference) |
| Current source files and maintenance | [Source index](UP-SELF-Developer-Handbook.md#appendix-c-source-and-maintenance-index) |

The two JSON files retain dependency evidence. Installed dependencies are still controlled by the POMs and frontend lockfile, not by a separate editable table in the Word document.

Three legacy Markdown paths remain for compatibility with unchanged startup helpers, version metadata and the Linux preparation verifier. Two are short English redirects; the FR compatibility index is generated from Appendix A. They are not separate manuals and should not be maintained by hand. Earlier standalone plans and delivery notes have been consolidated and removed from the active docs directory.

## Updating the Word edition

Edit the handbook Markdown, then run the optional documentation builder in a Python environment with python-docx installed:

```bash
python docs/tools/build_handbook.py
```

This rebuilds the Word file and compatibility entries. Open the result in Word and update the entire table of contents and all page fields before saving. In Windows Word, Select All and F9 refresh fields; the table of contents also offers Update Table. Use the corresponding update action in Mac Word. Review page breaks, long commands, table headers and local links before publishing.

To refresh only compatibility files without replacing the published Word file:

```bash
python docs/tools/build_handbook.py --compatibility-only
```

The builder is a documentation tool, not an application dependency. It never starts services, restores data or changes runtime versions. Keep render previews, temporary logs and intermediate PDFs outside this directory.
