/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.mamut.importer;

import org.mastodon.mamut.model.Model;
import org.mastodon.model.AbstractModelImporter;

public class ModelImporter extends AbstractModelImporter< Model >
{
	private final Model model;

	protected ModelImporter( final Model model )
	{
		super( model );
		this.model = model;
	}

	/**
	 * Pauses listeners attached to graph and tags. Graph and tags can be updated silently.
	 * Unpause with {@link #finishUpdate()}.
	 */
	@Override
	protected void startUpdate()
	{
		super.startUpdate();
		model.getTagSetModel().pauseListeners();
	}

	/**
	 * A counter action to {@link #startUpdate()}.
	 */
	protected void finishUpdate()
	{
	    //this is only an alias...
		finishImport();
	}

	/**
	 * Pauses listeners attached to graph and tags. Graph and tags are cleared allowing
	 * for a rebuilt from scratch (aka import). Unpause with {@link #finishImport()}.
	 */
	@Override
	protected void startImport()
	{
		super.startImport();
		model.getTagSetModel().pauseListeners();
		model.getTagSetModel().clear();
	}

	/**
	 * A counter action to {@link #startImport()}.
	 */
	@Override
	protected void finishImport()
	{
		model.getTagSetModel().resumeListeners();
		super.finishImport();
	}
}
