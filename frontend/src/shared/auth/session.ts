export type Channel = "student" | "admin";
export const session = {
  get token() {
    return sessionStorage.getItem("upself.token");
  },
  get channel(): Channel {
    return sessionStorage.getItem("upself.channel") === "admin"
      ? "admin"
      : "student";
  },
  save(token: string, channel: Channel) {
    sessionStorage.setItem("upself.token", token);
    sessionStorage.setItem("upself.channel", channel);
  },
  clear() {
    sessionStorage.removeItem("upself.token");
    sessionStorage.removeItem("upself.channel");
  },
};
