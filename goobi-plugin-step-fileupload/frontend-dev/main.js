import * as riot from 'riot'
import Uploader from './tags/uploader.tag'

const mountApp = riot.component(Uploader)

/* The goobiOpts look like this:
var options = {
    stepId: #{AktuelleSchritteForm.myPlugin.step.id},
    processId: #{AktuelleSchritteForm.myPlugin.step.prozess.id},
    folder: #{AktuelleSchritteForm.myPlugin.configFolder},
    userId: #{LoginForm.myBenutzer.id}
};
*/

window.mountUploader = mountApp;
