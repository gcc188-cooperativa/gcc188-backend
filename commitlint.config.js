// Conventional Commits com o módulo como escopo, ex.: feat(usuarios): inativar usuário (RF-USR-04)
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'scope-enum': [2, 'always', [
      'auth',
      'usuarios',
      'cooperados',
      'clientes',
      'contratos',
      'pagamentos',
      'despesas',
      'relatorios',
      'shared',
      'ci',
      'deps',
      'docs',
      'build',
      'config',
    ]],
    'header-max-length': [2, 'always', 100],
  },
};
