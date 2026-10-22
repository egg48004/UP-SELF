export function hasPermission(permissions: string[], permission: string) {
  return permissions.includes("*:*:*") || permissions.includes(permission);
}
export function hasAll(permissions: string[], required: string[]) {
  return required.every((p) => hasPermission(permissions, p));
}
