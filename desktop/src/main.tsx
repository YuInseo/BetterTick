import React from "react";
import ReactDOM from "react-dom/client";
import "./index.css";
import MainWindow from "./windows/MainWindow";
import QuickAdd from "./windows/QuickAdd";
import Pip from "./windows/Pip";

// One bundle, three windows: tauri.conf.json points each window at a hash route.
const route = window.location.hash.replace(/^#\/?/, "");
const transparent = route === "quickadd" || route === "pip";
if (transparent) document.documentElement.classList.add("transparent");

const View = route === "quickadd" ? QuickAdd : route === "pip" ? Pip : MainWindow;

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <View />
  </React.StrictMode>,
);
