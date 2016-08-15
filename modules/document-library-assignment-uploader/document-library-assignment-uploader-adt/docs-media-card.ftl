<#assign uploadAssignmentURL = renderResponse.createRenderURL() />

${uploadAssignmentURL.setParameter("mvcRenderCommandName", "/document_library/upload_assignment")}
${uploadAssignmentURL.setParameter("redirect", currentURL)}

<#if entries?has_content>
    <div class="card-row card-row-padded card-col-content">
        <label>
            <#list entries as entry>
            <div class="card">
                <div class="card-col-field">
				<img src="${dlUtil.getPreviewURL(entry, entry.getFileVersion(), themeDisplay, "")}" />
				</div>
				<div class="card-col-gutters">
		            <h4>${entry.title}</h4>
		            <p>${entry.description}</p>
		        </div>
		    </div>
            </#list>

			<a href="${uploadAssignmentURL}"><button class="btn btn-block btn-lg btn-primary" type="button">Upload Assignment</button</a>
        </label>
    </div>
</#if>
