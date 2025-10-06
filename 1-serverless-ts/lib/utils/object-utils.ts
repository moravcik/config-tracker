import { dot, pick } from 'dot-object';
import { isDeepStrictEqual } from 'node:util';
import { ConfigPathChangeType, ConfigPathChange } from '../types/config-path-change';


type DifferenceType = ConfigPathChangeType;
type Difference = ConfigPathChange;

export function calculateDifferences(obj1: any, obj2: any, flat = false): Difference[] {
  if (!obj1 && !obj2) return [];
  else if (!flat && !obj1) return [{ type: 'ADD', path: '', newValue: obj2 }];
  else if (!flat && !obj2) return [{ type: 'REMOVE', path: '', oldValue: obj1 }];

  const obj1Dot = dot(obj1), obj2Dot = dot(obj2);
  const allPaths = [...new Set([...Object.keys(obj1Dot), ...Object.keys(obj2Dot)])];

  const flatDiffs = allPaths.reduce((acc, key) => {
    const changeType = getDifferenceType(obj1Dot, obj2Dot, key);
    acc.push({type: changeType, path: key, oldValue: obj1Dot[key], newValue: obj2Dot[key]});
    return acc;
  }, [] as Difference[]);

  if (flat) return flatDiffs.filter(isDifference);

  let diffs = flatDiffs;
  let reducedDiffs: Difference[];
  do {
    reducedDiffs = mergeParentDifferences(obj1, obj2, diffs);
  } while (reducedDiffs.length < diffs.length && (diffs = reducedDiffs));

  return diffs.filter(isDifference);
}

function mergeParentDifferences(obj1: any, obj2: any, diffs: Difference[]): Difference[] {
  const {reducedDiffs} = diffs.reduce((acc, diff) => {
    if (acc.reducedPaths.includes(diff.path)) return acc; // already reduced

    const parentPath = diff.path.split('.').slice(0, -1).join('.');
    const allSiblings = diffs.filter(({path}) => path.startsWith(`${parentPath}.`));
    const allSiblingsEqualDiffType = allSiblings.every(({type}) => diff.type === type);

    if (parentPath.length && allSiblingsEqualDiffType) { // reduce to parent path
      acc.reducedPaths.push(...allSiblings.map(({path}) => path));
      acc.reducedDiffs.push({
        type: diff.type,
        path: parentPath,
        oldValue: pick(parentPath, obj1),
        newValue: pick(parentPath, obj2)
      });
    } else acc.reducedDiffs.push(diff); // pass otherwise
    return acc;
  }, { reducedDiffs: [] as Difference[], reducedPaths: [] as string[] });
  return reducedDiffs;
}

const isDifference: (diff: Difference) => boolean = ({type}) => type !== 'EQUAL';

function getDifferenceType(obj1Dot: any, obj2Dot: any, path: string): DifferenceType {
  const hasVal1 = obj1Dot.hasOwnProperty(path), hasVal2 = obj2Dot.hasOwnProperty(path);
  if (hasVal1 && hasVal2) {
    return isDeepStrictEqual(obj1Dot[path], obj2Dot[path]) ? 'EQUAL' : 'UPDATE';
  } else if (hasVal2) {
    return 'ADD';
  } else {
    return 'REMOVE';
  }
}
