/*
 * Copyright (c) 2025 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */

const origin = window.location.origin;
const path = window.location.pathname;
const query = window.location.search;
const hash = window.location.hash;

console.log("Origin: ", origin);
console.log("Path: ", path);
console.log("Query: ", query);
console.log("Hash: ", hash);

// Shows state content.
// Given Id needs to be one of 'loading-devspaces-text', 'register-developer-sandbox-text', 'error-text' or 'verify-account-text'.
function show(elementId) {
    console.log('showing element: ' + elementId);
    document.getElementById(elementId).style.display = 'block';
}

// Hides state content.
// Given Id needs to be one of 'loading-devspaces-text', 'register-developer-sandbox-text' or 'error-text'.
function hide(elementId) {
    console.log('hiding element: ' + elementId);
    document.getElementById(elementId).style.display = 'none';
}

function showError(errorText) {
  hideAll();
  show('error-text');
  show('error-status');
  document.getElementById('error-status').textContent = errorText;
}

// Redirects to the URL after 2 seconds
function redirect(url) {
    console.log("Redirect URL: ", url)
    setTimeout(function() {
        window.location.href = url;
    }, 2000);
}

function httpGetAsync(url, callback) {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
            callback(xmlHttp.responseText);
    }
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}

// Group mapping loaded on startup
let groupMapping = {};

// Load group mapping on startup
window.addEventListener('DOMContentLoaded', (event) => {
    // Load group mapping first with cache-busting
    fetch('/api/group-mapping', {
        cache: 'no-store',
        headers: {
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache',
            'Expires': '0'
        }
    })
        .then(response => response.json())
        .then(data => {
            groupMapping = data;
            console.log("Group mapping loaded:", groupMapping);
        })
        .catch(error => {
            console.error('Error fetching group mapping:', error);
        });

    // Load user info
    fetch('/api/user')
        .then(response => response.json())
        .then(data => {
            const userInfo = document.getElementById('user-info');
            if (data.user) {
                console.log("User: ", data.user);
                console.log("Groups: ", data.groups);
                console.log("Dev Spaces Mappings: ", data.devSpacesMappings);

                // Check if there's exactly one Dev Spaces URL, redirect to it
                if (data.devSpacesMappings && Array.isArray(data.devSpacesMappings) && data.devSpacesMappings.length === 1) {
                    const devSpacesUrl = data.devSpacesMappings[0].devSpacesUrl;
                    if (devSpacesUrl) {
                        console.log("Redirecting to Dev Spaces URL: ", devSpacesUrl);
                        show("loading-devspaces-text");
                        redirect(devSpacesUrl);
                        return; // Exit early since we're redirecting
                    }
                } else {
                    showError("User '" + data.user + "' is not allowed to access any of the configured Red Hat OpenShift Dev Spaces instances.");
                }
            } else {
                console.error('User info is not available');
            }
        })
        .catch(error => {
            console.error('Error fetching user info:', error);
        });
});
