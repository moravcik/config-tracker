
export type ConfigPathChangeType = 'ADD' | 'UPDATE' | 'REMOVE' | 'EQUAL'

export interface ConfigPathChange {
  type: ConfigPathChangeType;
  path: string;
  oldValue?: any;
  newValue?: any;
}