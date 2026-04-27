#!/bin/bash

echo "🔄 Syncing dev with main after merge..."

git checkout main && \
git pull origin main && \
git checkout dev && \
git merge main && \
git push origin dev

echo "✅ Done! dev is up to date with main."
