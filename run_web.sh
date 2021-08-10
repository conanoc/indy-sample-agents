#!/bin/bash

docker image build -t indy-js:1.0 -f indy-js.dockerfile .

server=`ipconfig getifaddr en0`:3000
docker run -e SERVER=$server -p 3000:3000 -v `pwd`/web:/web -w /web indy-js:1.0 sh -c "npm rebuild; node ./bin/www"
