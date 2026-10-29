export interface IdeaInput {
  title: string;
  offer: string;
  audience: string;
  resources: string;
  nextStep: string;
}
export interface BusinessIdea extends IdeaInput {
  id: number;
  version: number;
  createTime: string;
  updateTime: string;
}
export interface CostInput {
  materialsCost: string;
  otherCosts: string;
  quantity: string;
  pricePerItem: string;
}
export interface CostResult {
  materialsCost: string;
  otherCosts: string;
  quantity: number;
  pricePerItem: string;
  totalCost: string;
  expectedRevenue: string;
  estimatedProfit: string;
  currency: "MYR";
  version: number;
  saved: boolean;
  updateTime?: string;
}
