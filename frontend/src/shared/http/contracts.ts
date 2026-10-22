export interface Envelope {
  code: number;
  msg: string;
}
export interface DataResult<T> extends Envelope {
  data: T;
}
export interface TableResult<T> extends Envelope {
  rows: T[];
  total: number;
}
export interface PageQuery {
  current: number;
  pageSize: number;
  keyword?: string;
}
