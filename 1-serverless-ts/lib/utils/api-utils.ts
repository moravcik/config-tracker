export function okResult(body: any = '', statusCode: number = 200) {
  return transformResult({ statusCode, body });
}

export function errorResult(error: string | Object, statusCode: number = 500) {
  return transformResult({ statusCode, body: typeof error === 'string' ? { error } : error });
}

function transformResult({ statusCode = 200, body = ''}: { statusCode?: number, body?: any } = {}) {
  console.log('config API result', statusCode, body);
  return {
    statusCode,
    body: typeof body === 'string' ? body : JSON.stringify(body)
  };
}