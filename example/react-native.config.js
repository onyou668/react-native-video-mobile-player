const path = require('path');
const pkg = require('../package.json');
const { configureProjects } = require('react-native-test-app');
const libraryRoot = path.resolve(process.cwd(), '..');

module.exports = {
  project: configureProjects({
    android: {
      sourceDir: 'android',
    },
    ios: {
      sourceDir: 'ios',
      automaticPodsInstallation: true,
    },
  }),
  dependencies: {
    [pkg.name]: {
      root: libraryRoot,
      platforms: {
        // Codegen script incorrectly fails without this
        // So we explicitly specify the platforms with empty object
        ios: {},
        android: {},
      },
    },
  },
};
