const path = require('path');
const { workspace } = require('vscode');
const { LanguageClient } = require('vscode-languageclient/node');

let client;

function activate(context) {
    // <workspace-root>/target/dev-lang.jar
    // path.join(context.extensionPath, '..') resolve `./syntax` → workspace root
    const jarPath = path.join(context.extensionPath, '..', 'target', 'dev-lang.jar');

    const serverOptions = {
        command: 'java',
        args: ['-jar', jarPath, '--ls'],
        options: { cwd: path.join(context.extensionPath, '..') }
    };

    const clientOptions = {
        documentSelector: [{ scheme: 'file', language: 'lang' }],
        synchronize: {
            fileEvents: workspace.createFileSystemWatcher('**/*.lang')
        }
    };

    client = new LanguageClient(
        'langLSP',
        'Lang Language Server',
        serverOptions,
        clientOptions
    );
    client.start();
}

function deactivate() {
    return client ? client.stop() : undefined;
}

module.exports = { activate, deactivate };