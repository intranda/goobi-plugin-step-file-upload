const path = require("path")
const FileManagerPlugin = require('filemanager-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const webpack = require("webpack")

module.exports = {
    entry: './main.js',
    mode: "development",
    output: {
      path: path.resolve(__dirname, 'resources/dist/intranda_step_fileUpload/js/'),
      filename: 'uploader.js'
    },
    module: {
      rules: [
        {
          test: /\.tag$/,
          exclude: /node_modules/,
          use: [{
            loader: '@riotjs/webpack-loader',
            options: {
              hot: false, // set it to true if you are using hmr
              // add here all the other @riotjs/compiler options riot.js.org/compiler
              // template: 'pug' for example
            }
          }]
        },
        {
          test: /\.js$/,
          exclude: /node_modules/,
          use: {
            loader: 'babel-loader',
            options: {
              presets: ['@babel/preset-env']
            }
          }
        }
      ]
    },
    plugins: [
      new CopyWebpackPlugin({
        patterns: [
          {
            from: path.resolve(__dirname, 'resources/assets/plugin.js'),
            to: path.resolve(__dirname, 'resources/dist/intranda_step_fileUpload/js/plugin.js')
          },
          {
            from: path.resolve(__dirname, 'resources/assets/plugin.css'),
            to: path.resolve(__dirname, 'resources/dist/intranda_step_fileUpload/css/plugin.css')
          }
        ]
      })
    ]
  }