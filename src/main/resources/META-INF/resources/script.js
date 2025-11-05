/*
 * Copyright (c) 2021 Red Hat, Inc.
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
// Given Id needs to be one of 'loading-crw-text', 'register-developer-sandbox-text', 'error-text' or 'verify-account-text'.
function show(elementId) {
    console.log('showing element: ' + elementId);
    document.getElementById(elementId).style.display = 'block';
}

// Hides state content.
// Given Id needs to be one of 'loading-crw-text', 'register-developer-sandbox-text' or 'error-text'.
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

function httpGetAsync(url, callback) {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
            callback(xmlHttp.responseText);
    }
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}
