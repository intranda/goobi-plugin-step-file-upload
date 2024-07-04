const path = require("path")
const FileManagerPlugin = require('filemanager-webpack-plugin');
const webpack = require("webpack")

module.exports = {
    entry: './main.js',
    mode: "development",
    output: {
      path: path.resolve(__dirname, '../module-gui/src/main/webapp/resources/uii/plugins/step/intranda_step_fileUpload/js/'),
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
    }
  }