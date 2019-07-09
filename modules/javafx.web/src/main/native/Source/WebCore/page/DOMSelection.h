/*
 * Copyright (C) 2007 Apple Inc. All rights reserved.
 * Copyright (C) 2012 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of Apple Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "DOMWindowProperty.h"
#include "ExceptionOr.h"
#include <wtf/Forward.h>
#include <wtf/RefCounted.h>
#include <wtf/RefPtr.h>

namespace WebCore {

class Node;
class Position;
class Range;
class VisibleSelection;

class DOMSelection : public RefCounted<DOMSelection>, public DOMWindowProperty {
public:
    static Ref<DOMSelection> create(DOMWindow& window) { return adoptRef(*new DOMSelection(window)); }

    Node* baseNode() const;
    Node* extentNode() const;
    unsigned baseOffset() const;
    unsigned extentOffset() const;
    String type() const;
    void setBaseAndExtent(Node* baseNode, unsigned baseOffset, Node* extentNode, unsigned extentOffset);
    void setPosition(Node*, unsigned offset);
    void modify(const String& alter, const String& direction, const String& granularity);

    // The anchor and focus are the start and end of the selection, and
    // reflect the direction in which the selection was made by the user.
    // The base and extent are different, because they don't reflect expansion.
    Node* anchorNode() const;
    unsigned anchorOffset() const;
    Node* focusNode() const;
    unsigned focusOffset() const;
    bool isCollapsed() const;
    unsigned rangeCount() const;
    void collapse(Node*, unsigned offset);
    ExceptionOr<void> collapseToEnd();
    ExceptionOr<void> collapseToStart();
    ExceptionOr<void> extend(Node&, unsigned offset);
    ExceptionOr<Ref<Range>> getRangeAt(unsigned);
    void removeAllRanges();
    void addRange(Range&);
    void deleteFromDocument();
    bool containsNode(Node&, bool partlyContained) const;
    void selectAllChildren(Node&);

    String toString();

    void empty();

private:
    explicit DOMSelection(DOMWindow&);

    // Convenience method for accessors, caller must null-check frame().
    const VisibleSelection& visibleSelection() const;

    Node* shadowAdjustedNode(const Position&) const;
    unsigned shadowAdjustedOffset(const Position&) const;

    bool isValidForPosition(Node*) const;
};

} // namespace WebCore
