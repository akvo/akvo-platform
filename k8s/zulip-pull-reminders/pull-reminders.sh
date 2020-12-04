#!/bin/bash

set -euo pipefail

DATA_DIR=/tmp/akvo/github-pull-reminders

mkdir -p /tmp/akvo/github-pull-reminders

github_fetch() {
    HEADERS=()
    HEADERS+=("-HAccept: application/vnd.github.v3+json")
    if [[ -n "${GITHUB_TOKEN}" ]]
    then
       HEADERS+=("-HAuthorization: token ${GITHUB_TOKEN}")
    fi
    >&2 echo "Fetching data from ${1} ..."
    curl --silent --show-error --fail "${HEADERS[@]}" "$1"
}

download_repos() {
    github_fetch "https://api.github.com/orgs/akvo/repos?per_page=100&sort=updated" > "repos.json"
}

create_repos_list() {
    repos="$(jq -r '.[] | select(.archived == false) | .name' repos.json)"
    echo "$repos" > "repos.txt"
}

download_open_pulls_for_repo() {
    github_fetch "https://api.github.com/repos/akvo/$1/pulls?state=open" > "pulls-$1.json"
}

download_open_pulls_for_all_repos() {
    for repo in $(cat repos.txt)
    do
        download_open_pulls_for_repo "$repo"
        #list_open_pull_requests "$var"
    done
}

list_open_pulls_for_repo() {
    open_prs="$(jq -r '.[] | [(.requested_reviewers | map(.login) | join(";")), .title, .html_url]  | join("\t")' pulls-$1.json)"
    if [[ -n "$open_prs" ]]
    then
       echo -e "\n\n### ${1}\n"
       while read -r pr
       do
           columns=$(awk -F"\t" '{print NF}' <<< "${pr}")
           if [[ $columns = 3 ]] ; then
              pr_text=$(awk -F"\t" '{$1=""; print $0}' <<< "${pr}")
              devs=$(awk -F"\t" '{print $1}' <<< "${pr}" | tr ";" " ")
              for dev in ${devs}
              do
                  zname=$(grep "^${dev}" github-to-zulip.txt | awk -F: '{print $2}' || true)
                  if [[ -n "${zname}" ]]
                  then
                     devs=${devs/$dev/"@**${zname}**"}
                  fi
              done
              echo "${devs}: ${pr_text}"
           else
               echo "${pr}"
           fi
       done <<< "${open_prs}"
    fi
}

list_open_pulls_for_all_repos() {
    while read -r repo
    do
        list_open_pulls_for_repo "$repo"
    done < repos.txt
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
        --data 'include_custom_profile_fields=true' | jq -Mr '.members[]|.profile_data."1925".value+":"+.full_name' > github-to-zulip.txt
}

pushd ${DATA_DIR}
download_repos
create_repos_list
download_open_pulls_for_all_repos

post_to_zulip "$(list_open_pulls_for_all_repos)"
popd
