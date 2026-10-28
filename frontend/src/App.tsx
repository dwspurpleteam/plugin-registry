import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/AppLayout";
import { CreatePluginPage } from "./pages/CreatePluginPage";
import { ManifestPage } from "./pages/ManifestPage";
import { NewVersionPage } from "./pages/NewVersionPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { PluginVersionsPage } from "./pages/PluginVersionsPage";
import { PluginsPage } from "./pages/PluginsPage";

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/plugins" replace />} />
        <Route path="plugins" element={<PluginsPage />} />
        <Route path="plugins/new" element={<CreatePluginPage />} />
        <Route path="plugins/:pluginId/versions" element={<PluginVersionsPage />} />
        <Route path="plugins/:pluginId/versions/new" element={<NewVersionPage />} />
        <Route
          path="plugins/:pluginId/versions/:version/manifest"
          element={<ManifestPage />}
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
