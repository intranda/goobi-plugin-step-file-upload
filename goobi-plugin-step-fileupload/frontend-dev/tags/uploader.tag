<uploader>
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
  <script>
  export default {
    onBeforeMount(props, state) {
      this.state = {
      	  uploadFiles: [],
      	  msgs: {},
      	  currentUploadFile: -1,
      	  uploading: false
      };
      fetch(`/goobi/api/messages/${props.goobi_opts.language}`).then(resp => {
        resp.json().then(json => {
          this.state.msgs = json;
          this.update();
        })
      })
    },
    onMounted(props, state) {
      console.log("mounted", state.wantedMsgs);
    },
    onBeforeUpdate(props, state) {
      
    },
    onUpdated(props, state) {
      
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
	    for(var i=0;i<e.target.files.length; i++) {
	      e.target.files[i].uploaded = 0;
		  this.state.uploadFiles.push(e.target.files[i]);
	    }
	    this.update();
	    this.uploadNext();
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
	    xhr.upload.ontimeout = this.errorOnCurrent.bind(this);
	    xhr.upload.onprogress = this.progress.bind(this);
	    xhr.upload.onload = this.finishCurrentUpload.bind(this);
	    this.state.xhr = xhr;
	    xhr.send(formData);
	},
	progress(e) {
	    this.state.currentUpload.uploaded = e.loaded;
	    this.update();
	},
	finishCurrentUpload(e) {
	    if(e.status >= 400) {
	        console.log("error detected!")
	        return;
	    }
	    this.state.uploadFiles.shift();
	    this.uploadNext();
	    this.update();
	},
	errorOnCurrent(e) {
	    console.log("error", e);
	    //TODO: set error on current and abort uploading
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
        console.log(items)
	    for(var i=0;i<items.length;i++) {
	        items[i].uploaded=0;
	        this.state.uploadFiles.push(items[i]);
	    }
        this.uploadNext();
        this.update();
	}
  }
  </script>
</uploader>
