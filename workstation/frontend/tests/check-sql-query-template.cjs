// Run after installing the workstation's existing dependencies (no new dependency is required).
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const compiler = require('vue-template-compiler');

for (const name of ['SqlQuery.vue', 'SqlQueryResultExpand.vue']) {
  const filename = path.resolve(__dirname, '../src/business/sql-query', name);
  const component = compiler.parseComponent(fs.readFileSync(filename, 'utf8'));
  assert(component.template && component.script, `${name} must contain a template and script`);
  const compiled = compiler.compile(component.template.content, { outputSourceRange: true });
  assert.deepEqual(compiled.errors, [], `${name} must compile with Vue 2`);
  console.log(`${name}: Vue 2 template compilation passed`);
}
