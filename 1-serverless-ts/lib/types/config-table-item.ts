import { Config } from './config.schema';
import { ConfigPathChange } from './config-path-change';

export interface ConfigTableKeys {
  pk: string; // Partition Key - entityType#configId
  sk: string; // Sort Key - changeType?#timestamp#path?
}

export interface ConfigTableItem extends ConfigTableKeys {
  entityType: 'CONFIG' | 'CONFIG_CHANGE' | 'CONFIG_PATH_CHANGE';
  configId: string;
  timestamp: string; // ISO 8601 format
}

export interface ConfigItem extends ConfigTableItem {
  config: Config;
}

export interface ConfigChangeItem extends ConfigTableItem {
  pathChanges: ConfigPathChange[];
}

export interface ConfigPathChangeItem extends ConfigTableItem, ConfigPathChange {}
