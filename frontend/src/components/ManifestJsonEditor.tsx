import Editor, { loader } from "@monaco-editor/react";
import * as monaco from "monaco-editor/editor/editor.api";
import EditorWorker from "monaco-editor/editor/editor.worker?worker";
import JsonWorker from "monaco-editor/language/json/json.worker?worker";
import { jsonDefaults } from "monaco-editor/languages/features/json/register";
import { forwardRef, useEffect, useImperativeHandle, useRef } from "react";
import type { JsonSchema } from "../api/types";

const MANIFEST_MODEL_URI = "inmemory://plugin-registry/plugin-manifest-1.1.json";
const MANIFEST_SCHEMA_URI = "urn:plugin-registry:schema:plugin-manifest:1.1";

type MonacoEnvironment = {
  getWorker: (_moduleId: string, label: string) => Worker;
};

(self as unknown as { MonacoEnvironment: MonacoEnvironment }).MonacoEnvironment = {
  getWorker: (_moduleId, label) => label === "json" ? new JsonWorker() : new EditorWorker(),
};

loader.config({ monaco });

interface ManifestJsonEditorProps {
  value: string;
  schema: JsonSchema | null;
  onChange: (value: string) => void;
}

export interface ManifestJsonEditorHandle {
  showSuggestions: () => void;
}

export const ManifestJsonEditor = forwardRef<ManifestJsonEditorHandle, ManifestJsonEditorProps>(
  function ManifestJsonEditor({ value, schema, onChange }, ref) {
    const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);

    useImperativeHandle(ref, () => ({
      showSuggestions: () => {
        editorRef.current?.focus();
        editorRef.current?.trigger("manifest-editor", "editor.action.triggerSuggest", {});
      },
    }), []);

    useEffect(() => {
      jsonDefaults.setDiagnosticsOptions({
        validate: true,
        allowComments: false,
        enableSchemaRequest: false,
        schemaValidation: "error",
        schemas: schema
          ? [{ uri: MANIFEST_SCHEMA_URI, fileMatch: ["*"], schema }]
          : [],
      });
    }, [schema]);

    return (
      <Editor
        height="610px"
        language="json"
        path={MANIFEST_MODEL_URI}
        theme="vs-dark"
        value={value}
        onChange={(nextValue) => onChange(nextValue ?? "")}
        onMount={(editor) => {
          editorRef.current = editor;
        }}
        loading={<div className="editor-loading">Загрузка редактора…</div>}
        options={{
          automaticLayout: true,
          minimap: { enabled: false },
          fontFamily: '"SFMono-Regular", Consolas, monospace',
          fontSize: 12,
          lineHeight: 20,
          tabSize: 2,
          insertSpaces: true,
          formatOnPaste: true,
          quickSuggestions: true,
          suggestOnTriggerCharacters: true,
          wordBasedSuggestions: "off",
          scrollBeyondLastLine: false,
          padding: { top: 18, bottom: 18 },
          ariaLabel: "JSON-манифест плагина",
        }}
      />
    );
  },
);
