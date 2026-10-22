import type { Envelope } from "../../shared/http/contracts";
export interface User {
  id: number;
  userName: string;
  nickName: string;
  roleId: number;
  roleKey: string;
  status: string;
}
export interface Identity extends Envelope {
  totpEnabled: boolean;
  user: User;
  roles: string[];
  permissions: string[];
}
export interface Login extends Envelope {
  token: string;
}
