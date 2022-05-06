const fs = require("fs")
const path = require("path");
const ReactRefreshWebpackPlugin = require("@pmmmwh/react-refresh-webpack-plugin");

const appDirectory = fs.realpathSync(process.cwd());
const resolveApp = relativePath => path.resolve(appDirectory, relativePath);

module.exports = ({ proxyTarget }) => ({
  mode: "development",
  entry: {
    index: "./src/template-examples.tsx",
  },
  devtool: "inline-source-map",
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        exclude: /node_modules/,
        loader: require.resolve("babel-loader"),
        options: {
          plugins: [
            require.resolve("babel-plugin-macros"),
            require.resolve("react-refresh/babel"),
            [
              require.resolve("@babel/plugin-transform-runtime"),
              {
                regenerator: true,
              },
            ],
          ],
          presets: [
            require.resolve("@babel/preset-env"),
            require.resolve("@babel/preset-react"),
            require.resolve("@babel/preset-typescript"),
          ],
        },
      },
      {
        test: /\.css$/i,
        use: ["style-loader", "css-loader"],
      },
      {
        test: /\.(png|svg|jpg|jpeg|gif)$/i,
        type: "asset/inline",
      },
    ],
  },
  plugins: [new ReactRefreshWebpackPlugin()],
  resolve: {
    extensions: [".tsx", ".ts", ".js"],
    modules: [resolveApp('node_modules'), "node_modules"]
  },
  devServer: {
    hot: true,
    historyApiFallback: true,
    static: {
      directory: path.join(__dirname, "public"),
      watch: true,
    },
    proxy: {
      "/api": {
        target: proxyTarget ?? "https://dev.openk9.io",
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
