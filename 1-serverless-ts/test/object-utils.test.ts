import { calculateDifferences } from '../lib/utils/object-utils';

import exampleConfig from './example-config.json';
import exampleConfig2 from './example-config-v2.json';
import exampleConfig3 from './example-config-v3.json';

console.log(calculateDifferences(exampleConfig, exampleConfig3));
// console.log(calculateDifferences({}, exampleConfig));

const example1 = {
  "maxCreditLimit": 50000,
  "minCreditScore": 620,
  "currency": "EUR",
  "exceptions": [{
    "segment": "VIP",
    "maxCreditLimit": 150000,
    "requiresTwoManRule": true
  }]
};

const example2 = {
  "maxCreditLimit": 10000,
  "currency": "EUR",
  "exceptions": [{
    "segment": "BONUS",
    "maxCreditLimit": 30000,
    "requiresTwoManRule": false
  }]
}

console.log('---');
console.log(JSON.stringify(calculateDifferences(example1, example2), null, 2));