<uploader>
	<div class="uploader">
		<label class="btn btn-blue btn-file" if={state.uploadFiles.length == 0} ondragover={allowDrop} ondrop={onDrop}>
			{msg('selectFiles')}
	  		<input alt="file upload input" onchange={uploadFiles} type="file" multiple="multiple">
	  	</label>
	  	<ul class="uploadfiles" if={state.uploadFiles.length > 0}>
	  		<li each={(file, idx) in state.uploadFiles}>
	  			<div class="uploadfile">
	  				<span class="name">{file.name}</span>
	  				<div class="progress">
					 <div id="progress-{file.name}" class="progress-bar" role="progressbar" aria-valuenow="{file.uploaded}" aria-valuemin="0" aria-valuemax="{file.size}" style="width: {Math.floor(100*file.uploaded/file.size)}%;">
					    <span class="sr-only">{Math.floor(100*file.uploaded/file.size)}% Complete</span>
					  </div>
					</div>
	  				<span onclick={ (e) => cancel(e, idx)} class="btn"><i class="fa fa-times"></i></span>
	  			</div>
	  		</li>
	  	</ul>
  	</div>
  	<div if={state.errorFiles.length != 0} class="uploader-errors">
	  	<div each={errorFile in state.errorFiles} class="alert alert-danger">
			<span if={errorFile.error == 'namevalidation'}>
	  			{errorFile.name}: Filename is not allowed.
	  		</span>
	  		<span if={errorFile.error == 'upload'}>
	  			{errorFile.name}: Upload failed.
	  		</span>
	  	</div>
  	</div>
  <script>
  export default {
    onBeforeMount(props, state) {
      this.state = {
      	  uploadFiles: [],
      	  msgs: {},
      	  currentUploadFile: -1,
      	  uploading: false,
      	  errorFiles: []
      };
      fetch(`/goobi/api/messages/${props.goobi_opts.language}`).then(resp => {
        resp.json().then(json => {
          this.state.msgs = json;
          this.update();
        })
      })
    },
    onMounted(props, state) {
      
    },
    onBeforeUpdate(props, state) {
      
    },
    onUpdated(props, state) {
      
    },
    onBeforeUnmount(props, state) {
      console.log("unmounting - cancelling all uploads");
      for(var i=0;i<state.uploadFiles.length; i++){
      	this.cancel(null, 0);
      }
    },
    msg(str) {
      if(Object.keys(this.state.msgs).length == 0) {
          return "*".repeat(str.length);
      }
      if(this.state.msgs[str]) {
        return this.state.msgs[str];
      }
      return "???" + str + "???";
    },
    uploadFiles(e) {
      if(e.target.files.length >0) {
        var ok = this.validateFilenames(e.target.files);
        if(ok) {
		  for(var i=0;i<e.target.files.length; i++) {
		    e.target.files[i].uploaded = 0;
			this.state.uploadFiles.push(e.target.files[i]);
		  }
    	  this.uploadNext();
        }
	    this.update();
      }
	},
    uploadNext() {
	    if(this.state.uploadFiles.length == 0) {
	        return;
	    }
	    var fileToUpload = this.state.uploadFiles[0];
	    this.state.currentUpload = fileToUpload;
	    var formData = new FormData();
	    formData.append("file", fileToUpload);
	    var xhr = new XMLHttpRequest();
	    xhr.open("POST", `/goobi/api/processes/${this.props.goobi_opts.processId}/images/${this.props.goobi_opts.folder}`);
	    xhr.onerror = this.errorOnCurrent.bind(this);
	    xhr.onreadystatechange = function() {
	        console.log(xhr.readystate);
	    }
	    xhr.upload.ontimeout = this.errorOnCurrent.bind(this);
	    xhr.upload.onprogress = this.progress.bind(this);
	    xhr.onload = this.finishCurrentUpload.bind(this);
	    this.state.xhr = xhr;
	    xhr.send(formData);
	},
	progress(e) {
	    this.state.currentUpload.uploaded = e.loaded;
	    this.update();
	},
	finishCurrentUpload(e) {
	    if(e.target.status >= 400) {
	        console.log("error detected!")
	        var errorFile = this.state.uploadFiles.shift();
	    	this.state.errorFiles.push({name: errorFile.name, error: "upload"});
	    } else {
	    	this.state.uploadFiles.shift();
	    }
	    this.uploadNext();
	    this.update();
	},
	errorOnCurrent(e) {
	    console.log("error", e);
	    // set error on current and abort uploading
	    var errorFile = this.state.uploadFiles.shift();
	    this.state.errorFiles.push({name: errorFile.name, error: "upload"});
	    this.uploadNext();
	    this.update();
	},
	cancel(e, idx) {
    	this.state.uploadFiles.splice(idx, 1);
	    if(idx == 0) {
	        this.state.xhr.abort();
	        this.uploadNext();
	    }
	    this.update();
	},
	allowDrop(e) {
	    e.preventDefault();
	},
	onDrop(e) {
	    e.preventDefault();
	    console.log(e);
	    var items = e.dataTransfer.files;
        var ok = this.validateFilenames(items);
        if(ok) {
		    for(var i=0;i<items.length;i++) {
		        items[i].uploaded=0;
		        this.state.uploadFiles.push(items[i]);
		    }
	        this.uploadNext();
        }
        this.update();
	},
	validateFilenames(items) {
	    this.state.errorFiles = [];
	    console.log(this.props.goobi_opts.acceptRegex);
	    var ok = true;
	    for(var i=0;i<items.length;i++) {
	        if(! this.props.goobi_opts.acceptRegex.test(items[i].name)) {
	            this.state.errorFiles.push({name: items[i].name, error: 'namevalidation'});
	            ok = false;
	        }
	    }
	    return ok;
	}
  }
  </script>
</uploader>
