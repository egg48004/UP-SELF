import {describe,it,expect} from "vitest";
import {money,validAmount,validQuantity} from "../../src/features/business/presentation";
describe("business input and exact money presentation",()=>{
 it("preserves cents in large totals without binary floats",()=>{
  expect(money("9999999999990000.00")).toBe("RM 9,999,999,999,990,000.00");
  expect(money("-0.10")).toBe("−RM 0.10");
 });
 it("allows zero costs and two decimal places",()=>{
  for(const n of ["0","0.00","0.1","12.34","9999999999.99"])expect(validAmount(n)).toBe(true);
 });
 it("rejects ambiguous and invalid money inputs",()=>{
  for(const n of ["","-1","1.001","1e3","NaN","10000000000","1,000",".5"])expect(validAmount(n)).toBe(false);
 });
 it("requires a bounded whole-number quantity",()=>{
  for(const n of ["1","10","1000000"])expect(validQuantity(n)).toBe(true);
  for(const n of ["0","1.5","1000001","-1","","1e2"])expect(validQuantity(n)).toBe(false);
 });
});
