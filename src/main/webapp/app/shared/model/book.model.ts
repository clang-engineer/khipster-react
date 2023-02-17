export interface IBook {
  id?: number;
  title?: string;
  description?: string | null;
}

export const defaultValue: Readonly<IBook> = {};
