import vue from 'eslint-plugin-vue'; import ts from 'typescript-eslint';
export default ts.config(...ts.configs.recommended,...vue.configs['flat/recommended'],{files:['**/*.vue'],languageOptions:{parserOptions:{parser:ts.parser}}},{rules:{'vue/multi-word-component-names':'off','vue/html-self-closing':'off','vue/max-attributes-per-line':'off','@typescript-eslint/no-explicit-any':'error'}});
