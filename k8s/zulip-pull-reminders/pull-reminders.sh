#!/bin/bash

set -euo pipefail

# rm -rf /tmp/akvo/github-pull-reminders
mkdir -p /tmp/akvo/github-pull-reminders

github_fetch() {
    HEADERS=()
    HEADERS+=("-HAccept: application/vnd.github.v3+json")
    if [[ -n "${GITHUB_TOKEN}" ]];
    then
    HEADERS+=("-HAuthorization: token ${GITHUB_TOKEN}")
    fi
    >&2 echo "Fetching data from ${1} ..."
    curl --silent --show-error --fail "${HEADERS[@]}" "$1"
}

download_repos() {
    github_fetch https://api.github.com/orgs/akvo/repos?per_page=100 > "/tmp/akvo/github-pull-reminders/repos.json"
}

create_repos_list() {
    repos="$(jq -r '.[].name' /tmp/akvo/github-pull-reminders/repos.json)"
    echo "$repos" > "/tmp/akvo/github-pull-reminders/repos.txt"
}

download_open_pulls_for_repo() {
    github_fetch "https://api.github.com/repos/akvo/$1/pulls?state=open" > "/tmp/akvo/github-pull-reminders/pulls-$1.json"
}

download_open_pulls_for_all_repos() {
    for repo in $(cat /tmp/akvo/github-pull-reminders/repos.txt)
    do
        download_open_pulls_for_repo "$repo"
        #list_open_pull_requests "$var"
    done
}

list_open_pulls_for_repo() {
    open_prs="$(jq -r '.[] | [.title, (.requested_reviewers | map(.login) | join(", ")), .html_url]  | join(" ")' /tmp/akvo/github-pull-reminders/pulls-$1.json)"
    if [ ! -z "$open_prs" ]
    then
       printf "\n\n### $1\n"
       printf "$open_prs"
    fi
}

list_open_pulls_for_all_repos() {
    for repo  in $(cat /tmp/akvo/github-pull-reminders/repos.txt)
    do
        list_open_pulls_for_repo "$repo"
    done
}

post_to_zulip(){
    curl -X POST https://akvo.zulipchat.com/api/v1/messages \
         -u "${ZULIP_TOKEN}" \
         -d 'type=stream' \
         -d 'to=bot-test' \
         -d 'topic=Pull reminder' \
         -d "content=$1"
}

github_username_to_zulip_name() {
    curl --silent --show-error --fail \
        --get https://akvo.zulipchat.com/api/v1/users \
        --user "${ZULIP_TOKEN}" \
        --data 'include_custom_profile_fields=true' | jq -Mr '.members[]|.profile_data."1925".value+":"+.full_name' > /tmp/akvo/github-pull-reminders/github-to-zulip.txt
}

download_repos
create_repos_list
download_open_pulls_for_all_repos

post_to_zulip "$(list_open_pulls_for_all_repos)"
