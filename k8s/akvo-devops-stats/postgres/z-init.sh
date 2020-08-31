#!/usr/bin/env bash

set -eu

psql -c "CREATE ROLE devopsstatsdbuser WITH PASSWORD 'xxxxpasswd' CREATEDB LOGIN;"
psql -c "CREATE DATABASE devopsstatsdb WITH OWNER = devopsstatsdbuser TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8';"