#!/bin/bash

# For testing production mode e2e tests we need some tweaks:
# 1) The angular app needs to run in dev mode for protractor to work
# 2) We update the basehref of the angular app to the context path of the backend
# 3) We update the protractor configuration to the new path
#
# Then we start up a docker stack with a postgres server since production mode is configured for a
# postgres database instead of in-memory database.

set -ev

sed -i "s|new plugin.BaseHrefWebpackPlugin({ baseHref: '/' })|new plugin.BaseHrefWebpackPlugin({ baseHref: '/managementportal/' })|" webpack/webpack.dev.js
sed -i "s|baseUrl: 'http://localhost:8080/',|baseUrl: 'http://localhost:8080/managementportal/',|" src/test/javascript/protractor.conf.js
./gradlew bootRepackage -Pprod buildDocker -x test
docker-compose -f src/main/docker/app.yml up -d # spin up production mode application
# wait for app to start up
echo -n "Waiting for application startup"
until curl -s http://localhost:8080/managementportal/ > /dev/null
do
  echo -n "." # waiting
  sleep 2
done
echo ""
echo "Application is up"
docker-compose -f src/main/docker/app.yml logs # show output of app startup
yarn e2e # run e2e tests against production mode
docker-compose -f src/main/docker/app.yml down -v # clean up containers and volumes
git checkout src/test/javascript/protractor.conf.js
git checkout webpack/webpack.prod.js