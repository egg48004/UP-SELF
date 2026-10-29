// Keep money as decimal strings. Large totals must not pass through binary floats.
export function money(value: string) {
  const negative = value.startsWith("-");
  const [whole, fraction = ""] = value.replace(/^-/u, "").split(".");
  return (
    (negative ? "−" : "") +
    "RM " +
    whole!.replace(/\B(?=(\d{3})+(?!\d))/gu, ",") +
    "." +
    fraction.padEnd(2, "0")
  );
}
export function validAmount(value: string) {
  return /^\d{1,10}(?:\.\d{1,2})?$/u.test(value);
}
export function validQuantity(value: string) {
  return (
    /^\d{1,7}$/u.test(value) && Number(value) >= 1 && Number(value) <= 1000000
  );
}
