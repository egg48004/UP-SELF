本轮新增 `features/dashboard`、`features/assignments`，角色入口、接口和验收见 [交付说明](../docs/三角色与作业模块交付验收.md)。业务页面通过各自 api.ts 调用公共认证请求客户端；作业状态集中在 useStudentWork，附件及历史组件可独立维护。

# UP-SELF frontend

Vue learning application. Run the backend on 8080 first.

Use the workspace F5 configuration **UP-SELF · Frontend** or run `../frontend.cmd dev` from this directory. `../frontend.cmd build`, `typecheck`, `lint`, `test` use the same pinned Node/npm. Visit http://127.0.0.1:5173 .

Dependencies are pinned in package.json and package-lock.json. Student and management layouts share the request client, identity, preferences and feature components. No assignments or mood pages are included yet.
