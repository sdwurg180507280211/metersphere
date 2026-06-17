const ABSOLUTE_URL_PATTERN = /^https?:\/\//i;

const normalizePath = (path) => {
  if (!path) {
    return "/";
  }
  return path.startsWith("/") ? path : `/${path}`;
};

const trimTrailingSlash = (value) => value.replace(/\/+$/, "");

export const buildApitableUrl = (baseUrl, defaultPath) => {
  const path = defaultPath || "/";
  if (ABSOLUTE_URL_PATTERN.test(path)) {
    return path;
  }
  if (!baseUrl) {
    return "";
  }
  return `${trimTrailingSlash(baseUrl)}${normalizePath(path)}`;
};

const getRuntimeConfig = () => {
  if (typeof window === "undefined") {
    return {};
  }
  return window.__MS_APITABLE_CONFIG__ || {};
};

const toBoolean = (value, fallback) => {
  if (value === undefined || value === null || value === "") {
    return fallback;
  }
  return ["true", "1", "yes", "on"].includes(String(value).toLowerCase());
};

const toPositiveNumber = (value, fallback) => {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : fallback;
};

const firstConfigured = (...values) => values.find((value) => value !== undefined && value !== null && value !== "");

export const getApitableEmbedConfig = () => {
  const runtimeConfig = getRuntimeConfig();
  const baseUrl = firstConfigured(runtimeConfig.baseUrl, process.env.VUE_APP_APITABLE_BASE_URL, "");
  const defaultPath = firstConfigured(runtimeConfig.defaultPath, process.env.VUE_APP_APITABLE_DEFAULT_PATH, "/");
  const embedUrl = firstConfigured(
    runtimeConfig.embedUrl,
    process.env.VUE_APP_APITABLE_EMBED_URL,
    buildApitableUrl(baseUrl, defaultPath)
  );

  return {
    embedUrl,
    openInNewWindow: toBoolean(
      firstConfigured(runtimeConfig.openInNewWindow, process.env.VUE_APP_APITABLE_OPEN_IN_NEW_WINDOW),
      true
    ),
    loadTimeout: toPositiveNumber(
      firstConfigured(runtimeConfig.loadTimeout, process.env.VUE_APP_APITABLE_LOAD_TIMEOUT),
      15000
    ),
  };
};

export const mergeApitableEmbedConfig = (serverConfig = {}) => {
  const runtimeConfig = getRuntimeConfig();
  const baseUrl = firstConfigured(
    runtimeConfig.baseUrl,
    process.env.VUE_APP_APITABLE_BASE_URL,
    serverConfig.baseUrl,
    ""
  );
  const defaultPath = firstConfigured(
    runtimeConfig.defaultPath,
    process.env.VUE_APP_APITABLE_DEFAULT_PATH,
    serverConfig.defaultPath,
    "/"
  );
  const embedUrl = firstConfigured(
    runtimeConfig.embedUrl,
    process.env.VUE_APP_APITABLE_EMBED_URL,
    serverConfig.embedUrl,
    buildApitableUrl(baseUrl, defaultPath)
  );

  return {
    embedUrl,
    openInNewWindow: toBoolean(
      firstConfigured(
        runtimeConfig.openInNewWindow,
        process.env.VUE_APP_APITABLE_OPEN_IN_NEW_WINDOW,
        serverConfig.openInNewWindow
      ),
      true
    ),
    loadTimeout: toPositiveNumber(
      firstConfigured(runtimeConfig.loadTimeout, process.env.VUE_APP_APITABLE_LOAD_TIMEOUT, serverConfig.loadTimeout),
      15000
    ),
  };
};
