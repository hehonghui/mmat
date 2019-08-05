#!/bin/bash

# 该脚本用于git提交并且push到远程分支上, 参数1要提交的log

# 获取当前所在分支
curBranch=$(git symbolic-ref --short -q HEAD)
echo "                                ==========> 提交代码到本地, 分支为: $curBranch"

git add .
git commit -m "$1"
if [ "$curBranch" == "dev" -o "$curBranch" == "comment-dev" ];then
    ./ps_to_github.sh
fi
