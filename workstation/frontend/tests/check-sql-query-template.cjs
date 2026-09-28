// Run after installing the workstation's existing dependencies (no new dependency is required).
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const compiler = require('vue-template-compiler');

const filename = path.resolve(__dirname, '../src/business/sql-query/SqlQueryResultExpand.vue');
const component = compiler.parseComponent(fs.readFileSync(filename, 'utf8'));
assert(component.template && component.script, 'The workbench must contain a template and script');
const compiled = compiler.compile(component.template.content, { outputSourceRange: true });
assert.deepEqual(compiled.errors, [], 'The workbench template must compile with Vue 2');
console.log('SQL query workbench Vue template compilation passed');
