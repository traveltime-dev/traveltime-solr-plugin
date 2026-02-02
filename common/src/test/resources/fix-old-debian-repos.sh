#!/bin/bash
# Script to fix apt repositories for old Debian versions (Jessie and Stretch)
# that have been moved to archive.debian.org

set -ex

# Read the Debian codename from sources.list
CODENAME=""
if grep -q "jessie" /etc/apt/sources.list 2>/dev/null; then
    CODENAME="jessie"
elif grep -q "stretch" /etc/apt/sources.list 2>/dev/null; then
    CODENAME="stretch"
fi

# If we found an old Debian version, apply the fix
if [ -n "$CODENAME" ]; then
    # Update main repository to archive
    sed -i 's|http://deb.debian.org/debian|http://archive.debian.org/debian|g' /etc/apt/sources.list

    # Remove security repository lines (not available for archived versions)
    sed -i '/security\.debian\.org/d' /etc/apt/sources.list

    # Remove updates lines (not available for archived versions)
    sed -i "/${CODENAME}-updates/d" /etc/apt/sources.list

    # Remove backports lines (not available for archived versions)
    sed -i "/${CODENAME}-backports/d" /etc/apt/sources.list
    sed -i '/backports/d' /etc/apt/sources.list

    # Also check and fix sources.list.d directory
    if [ -d /etc/apt/sources.list.d ]; then
        for file in /etc/apt/sources.list.d/*.list; do
            if [ -f "$file" ]; then
                sed -i "/${CODENAME}-updates/d" "$file"
                sed -i "/${CODENAME}-backports/d" "$file"
                sed -i '/backports/d' "$file"
                sed -i '/security\.debian\.org/d' "$file"
            fi
        done
    fi

    # Configure apt to not check Valid-Until for archived packages
    echo 'Acquire::Check-Valid-Until false;' > /etc/apt/apt.conf.d/99archive

    # Configure apt to allow unauthenticated packages (GPG keys expired)
    echo 'APT::Get::AllowUnauthenticated "true";' >> /etc/apt/apt.conf.d/99archive

    # Ignore GPG errors for expired keys
    echo 'Acquire::AllowInsecureRepositories "true";' >> /etc/apt/apt.conf.d/99archive
    echo 'Acquire::AllowDowngradeToInsecureRepositories "true";' >> /etc/apt/apt.conf.d/99archive

    echo "Repository fix applied for $CODENAME"
fi
