import { calculateDifferences } from '../lib/utils/object-utils';

import exampleConfig from './example-config.json';
import exampleConfig2 from './example-config-v2.json';
import exampleConfig3 from './example-config-v3.json';

console.log(calculateDifferences(exampleConfig, exampleConfig3));
// console.log(calculateDifferences({}, exampleConfig));