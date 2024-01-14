/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.module.indexer.post

import io.jrb.labs.iotindexerms.model.Post
import io.jrb.labs.module.indexer.MessageIndexer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Instant

@Component
class PostIndexer(private val postDocumentRepository: PostDocumentRepository) : MessageIndexer<Post> {

    override fun index(message: Post): Flow<Any> {
        val timestamp = Instant.now()
        return postDocumentRepository.findByPostId(message.id)
            .switchIfEmpty { Mono.just(PostDocument(postId = message.id, createdOn = timestamp)) }
            .map { it.copy(
                title = message.title,
                modifiedOn = timestamp
            ) }
            .flatMap { postDocumentRepository.save(it) }
            .asFlow()
    }

}