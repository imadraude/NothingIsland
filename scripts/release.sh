#!/data/data/com.termux/files/usr/bin/env bash
set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

GRADLE_FILE="app/build.gradle.kts"
CHANGELOG_FILE="CHANGELOG.md"

if [ ! -f "$GRADLE_FILE" ]; then
    echo -e "${RED}Error: $GRADLE_FILE not found! Run from project root.${NC}" >&2
    exit 1
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    echo -e "${YELLOW}Warning: Working directory has uncommitted changes.${NC}"
    read -p "Do you want to continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 1
    fi
fi

# Extract current version
CURRENT_VERSION=$(grep 'versionName = ' "$GRADLE_FILE" | head -n 1 | sed -E 's/.*"([^"]+)".*/\1/')

if [ -z "$CURRENT_VERSION" ]; then
    echo -e "${RED}Could not extract current version from $GRADLE_FILE${NC}" >&2
    exit 1
fi

echo -e "Current SemVer: ${BLUE}v$CURRENT_VERSION${NC}"

# Parse semver parts
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
# In case patch has pre-release metadata, extract only numeric
PATCH=$(echo "$PATCH" | sed -E 's/([0-9]+).*/\1/')

BUMP_TYPE="${1:-patch}"

case "$BUMP_TYPE" in
    patch)
        NEW_PATCH=$((PATCH + 1))
        NEW_VERSION="$MAJOR.$MINOR.$NEW_PATCH"
        ;;
    minor)
        NEW_MINOR=$((MINOR + 1))
        NEW_VERSION="$MAJOR.$NEW_MINOR.0"
        ;;
    major)
        NEW_MAJOR=$((MAJOR + 1))
        NEW_VERSION="$NEW_MAJOR.0.0"
        ;;
    *)
        # Check if argument is a valid SemVer X.Y.Z
        if [[ "$BUMP_TYPE" =~ ^[0-9]+\.[0-9]+\.[0-9]+ ]]; then
            NEW_VERSION="$BUMP_TYPE"
        else
            echo -e "${RED}Invalid bump type or version: '$BUMP_TYPE'${NC}"
            echo "Usage: $0 [patch|minor|major|<X.Y.Z>]"
            exit 1
        fi
        ;;
esac

echo -e "Bumping version: ${YELLOW}v$CURRENT_VERSION${NC} ➔ ${GREEN}v$NEW_VERSION${NC}"

# 1. Update app/build.gradle.kts
sed -i -E "s/versionName = \"[^\"]+\"/versionName = \"$NEW_VERSION\"/" "$GRADLE_FILE"

# 2. Update CHANGELOG.md if exists
TODAY=$(date +%Y-%m-%d)
if [ -f "$CHANGELOG_FILE" ]; then
    if grep -q "## \[Unreleased\]" "$CHANGELOG_FILE"; then
        sed -i -E "s/## \[Unreleased\]/## [Unreleased]\n\n## [$NEW_VERSION] - $TODAY/" "$CHANGELOG_FILE"
    fi
fi

# 3. Git commit and tag
git add "$GRADLE_FILE" "$CHANGELOG_FILE"
git commit -m "chore(release): bump version to v$NEW_VERSION"
git tag -a "v$NEW_VERSION" -m "Release v$NEW_VERSION"

echo -e "${GREEN}✓ Successfully released v$NEW_VERSION!${NC}"
echo -e "To push release and trigger GitHub Actions build:"
echo -e "${BLUE}git push origin $(git rev-parse --abbrev-ref HEAD) --tags${NC}"
