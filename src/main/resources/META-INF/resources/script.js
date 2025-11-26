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
// Given Id needs to be one of 'loading-devspaces-text', 'error-text'
function show(elementId) {
    console.log('showing element: ' + elementId);
    document.getElementById(elementId).style.display = 'block';
}

// Hides state content.
// Given Id needs to be one of 'loading-devspaces-text', 'error-text'
function hide(elementId) {
    console.log('hiding element: ' + elementId);
    document.getElementById(elementId).style.display = 'none';
}

// Hides all state content.
function hideAll() {
    console.log('hiding all...');
    document.getElementById('loading-devspaces-text').style.display = 'none';
    document.getElementById('error-text').style.display = 'none';
    document.getElementById('error-status').style.display = 'none';
    document.getElementById('devspaces-selection').style.display = 'none';
  }

function showError(errorText) {
  hideAll();
  show('error-text');
  show('error-status');
  document.getElementById('error-status').textContent = errorText;
}

// Redirects to the URL after 2 seconds
function redirect(url) {
    // Build redirect URL preserving pathname, query, and hash from original URL
    let redirectUrl = url;
    
    try {
        // Parse the base URL
        const urlObj = new URL(url);
        
        // Append pathname if it exists and is not just "/"
        if (path && path !== '/') {
            // Replace any existing path with the original path
            urlObj.pathname = path;
        }
        
        // Append query string if it exists
        if (query) {
            // If URL already has query params, merge them, otherwise just append
            if (urlObj.search) {
                urlObj.search = urlObj.search + (urlObj.search.endsWith('&') ? '' : '&') + query.substring(1);
            } else {
                urlObj.search = query;
            }
        }
        
        // Append hash/anchor if it exists in the original URL
        if (hash) {
            urlObj.hash = hash;
        }
        
        redirectUrl = urlObj.toString();
    } catch (e) {
        // If URL parsing fails (e.g., relative URL), use string manipulation
        console.warn("Failed to parse URL, using string manipulation:", e);
        
        // Remove hash and query from base URL if they exist
        const hashIndex = url.indexOf('#');
        const queryIndex = url.indexOf('?');
        let baseUrl = url;
        
        // Find the end of the base URL (before query or hash)
        let urlEnd = url.length;
        if (hashIndex !== -1) urlEnd = Math.min(urlEnd, hashIndex);
        if (queryIndex !== -1) urlEnd = Math.min(urlEnd, queryIndex);
        baseUrl = url.substring(0, urlEnd);
        
        // Build redirect URL with path, query, and hash
        redirectUrl = baseUrl;
        
        // Append pathname if it exists and is not just "/"
        if (path && path !== '/') {
            // Remove trailing slash from base URL if present, then append path
            redirectUrl = baseUrl.replace(/\/$/, '') + path;
        }
        
        // Append query string if it exists
        if (query) {
            redirectUrl += query;
        }
        
        // Append hash if it exists
        if (hash) {
            redirectUrl += hash;
        }
    }
    
    console.log("Redirect URL: ", redirectUrl)
    setTimeout(function() {
        window.location.href = redirectUrl;
    }, 2000);
}

// Shows Dev Spaces selection UI when multiple instances are available
function showDevSpacesSelection(mappings) {
    hideAll();
    const selectionDiv = document.getElementById('devspaces-selection');
    const optionsDiv = document.getElementById('devspaces-options');
    
    // Clear any existing options
    optionsDiv.innerHTML = '';
    
    // Create a button for each Dev Spaces instance
    mappings.forEach((mapping, index) => {
        const button = document.createElement('button');
        button.className = 'devspaces-option-button';
        button.textContent = mapping.group || `Dev Spaces Instance ${index + 1}`;
        button.title = mapping.devSpacesUrl || ''; // Show redirect URL on hover
        button.onclick = function() {
            console.log("Selected Dev Spaces URL: ", mapping.devSpacesUrl);
            selectionDiv.style.display = 'none';
            show("loading-devspaces-text");
            redirect(mapping.devSpacesUrl);
        };
        optionsDiv.appendChild(button);
    });
    
    selectionDiv.style.display = 'block';
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
                if (data.devSpacesMappings && Array.isArray(data.devSpacesMappings)) {
                    if (data.devSpacesMappings.length === 1) {
                        const devSpacesUrl = data.devSpacesMappings[0].devSpacesUrl;
                        if (devSpacesUrl) {
                            console.log("Redirecting to Dev Spaces URL: ", devSpacesUrl);
                            show("loading-devspaces-text");
                            redirect(devSpacesUrl);
                            return; // Exit early since we're redirecting
                        }
                    } else if (data.devSpacesMappings.length > 1) {
                        // Show selection UI for multiple Dev Spaces
                        showDevSpacesSelection(data.devSpacesMappings);
                        return;
                    }
                }
                
                // No Dev Spaces mappings found
                if (!data.devSpacesMappings || data.devSpacesMappings.length === 0) {
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
